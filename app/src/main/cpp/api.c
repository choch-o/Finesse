#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <fcntl.h>
#include <unistd.h>
#include <arpa/inet.h>
//#include <android/log.h>

//#include <android/log.h>

#include "lbs.h"
#include "core.h"

layout_elm_t *
lbs_new_layout_elm(int x, int y, int width, int height, const char *extra, int extralen)
{
	layout_elm_t *elm;

	if (!extra)
		extralen = 0;

	if (!(elm = (layout_elm_t *)calloc(sizeof(layout_elm_t) + extralen, 1)))
		return NULL;

	elm->x = x;
	elm->y = y;
	elm->width = width;
	elm->height = height;

	elm->hash = 0;

	elm->child = NULL;
	elm->sibling = NULL;

	if (extra) {
		elm->extralen = extralen;
		memcpy(elm->extra, extra, extralen);
	} else {
		elm->extralen = 0;
	}

	return elm;
}

void
_lbs_del_layout_elm(lbs_ctx_t *lctx, layout_elm_t *elm)
{
	free(elm);
}

void
lbs_del_layout_elm(layout_elm_t *elm)
{
	_lbs_del_layout_elm(NULL, elm);
}

void
lbs_del_layout_recursive(layout_elm_t *root)
{
	_lbs_dfs_elm(NULL, root, _lbs_del_layout_elm);
}

int
lbs_add_child(layout_elm_t *parent, layout_elm_t *child)
{
	//__android_log_print(ANDROID_LOG_DEBUG, "LbsNative", "check 0");
	layout_elm_t *elm;

	if (!parent || !child)
		return -1;

	if (!parent->child) {
		parent->child = child;
		return 0;
	}

	for (elm = parent->child;
		 elm->sibling;
		 elm = elm->sibling)
		if (elm->sibling == child)
			break;

	elm->sibling = child;

	return 0;
}

int
lbs_commit_frame(lbs_ctx_t *ctx, layout_elm_t *root, struct timeval *tv)
{
	frame_t frame;
	struct timeval _tv;

	if (!ctx || !root)
		return -1;

	if (!tv) {
		gettimeofday(&_tv, NULL);
		tv = &_tv;
	}

	_lbs_commit_elm_recursive(ctx, root);

	frame.tv_sec = htonl((uint32_t)tv->tv_sec);
	frame.tv_usec = htonl((uint32_t)tv->tv_usec);
	frame.root = root->hash;

	_lbs_write(ctx->log_fd, (uint8_t *)&frame, sizeof(frame_t));

	fsync(ctx->lt_fd);
	fsync(ctx->db_fd);
	fsync(ctx->log_fd);

	return 0;
}

size_t
_get_filesize(int fd)
{
	unsigned long cur_pos, pos;

	cur_pos = lseek(fd, 0, SEEK_CUR);

	pos = lseek(fd, 0, SEEK_END);

	lseek(fd, cur_pos, SEEK_SET);

	return (size_t)pos;
}

int
_load_lbs_file(const char *path, int oflag)
{
	lbs_file_t lbs_header = {0};
	int fd;

	if (oflag & O_RDONLY) {
		return open(path, oflag, 0);
	}

	if (!(fd = open(path, oflag, 0664)))
		return -1;

	if (_get_filesize(fd) < sizeof(lbs_file_t)) {
		lseek(fd, 0, SEEK_SET);
		_lbs_write(fd, (uint8_t *)&lbs_header, sizeof(lbs_file_t));
	}

	return fd;
}

int
lbs_init_ctx(lbs_ctx_t *ctx, const char *ltpath, const char *dbpath, const char *logpath, bool rdonly)
{
	off_t pos;
	size_t filesize;
	int oflag;

	if (!ctx)
		return -1;

	if (rdonly)
		oflag = O_RDONLY;
	else
		oflag = O_RDWR | O_CREAT | O_APPEND;

	if (!ltpath || (ctx->lt_fd = _load_lbs_file(ltpath, oflag)) < 0)
		return -1;

	if (!dbpath || (ctx->db_fd = _load_lbs_file(dbpath, oflag)) < 0)
		return -1;

	if (!logpath || (ctx->log_fd = _load_lbs_file(logpath, oflag)) < 0)
		return -1;

	if (strlen(ltpath) < 128 && strlen(dbpath) < 128 && strlen(logpath) < 128) {
		ctx->oflag = oflag;
		strncpy(ctx->ltpath, ltpath, 128);
		strncpy(ctx->dbpath, dbpath, 128);
		strncpy(ctx->logpath, logpath, 128);
		ctx->resumable = 1;
	} else
		ctx->resumable = 0;

	ctx->lt = kvs_create(102400/*, 1024000*/);

	filesize = _get_filesize(ctx->lt_fd);
	for (pos = lseek(ctx->lt_fd, sizeof(lbs_file_t), SEEK_SET);
		 filesize - pos >= sizeof(lookup_ent_t);
		 pos = lseek(ctx->lt_fd, pos + sizeof(lookup_ent_t), SEEK_SET)) {
		// printf("pos: %lld, filesize: %lu\n", pos, filesize);

		lookup_ent_t ent;

		read(ctx->lt_fd, (uint8_t *)&ent, sizeof(lookup_ent_t));

		kvs_insert(ctx->lt, ent.hash, (_value_t)ntohl(ent.offset));
	}

	return 0;
}

int
lbs_destroy_ctx(lbs_ctx_t *ctx)
{
	if (!ctx)
		return -1;

	close(ctx->lt_fd);
	close(ctx->db_fd);
	close(ctx->log_fd);

	kvs_destroy(ctx->lt);

	return 0;
}

void
_lbs_print_layout_recursive(lbs_ctx_t *ctx, hash_t hash, int xoff, int yoff)
{
	/* 200629 dhkim: Sorry for dirty code here. I am running out of time */
	layout_ent_t ent;
	content_ent_t cont;
	hash_t *children = NULL;
	char *data = NULL;
	uint32_t offset;
	int i;

	// printf("looking for %08X\n", hash);

	/* read layout */ 
	if ((offset = kvs_search(ctx->lt, hash)) == 0)
		return;

	lseek(ctx->db_fd, offset, SEEK_SET);
	read(ctx->db_fd, (uint8_t *)&ent, sizeof(layout_ent_t));

	if (ent.nc && (children = (hash_t *)malloc(sizeof(hash_t) * ent.nc)))
		read(ctx->db_fd, (uint8_t *)children, sizeof(hash_t) * ent.nc);

	// printf("nc: %u, offset: %u\n", ent.nc, offset);

	// printf("content hash: %08X\n", ent.content);
	/* read content */ 
	if ((offset = kvs_search(ctx->lt, ent.content)) != 0) {
		lseek(ctx->db_fd, offset, SEEK_SET);
		read(ctx->db_fd, (uint8_t *)&cont, sizeof(content_ent_t));

		int datalen = ntohs(cont.datalen);
		if (datalen && (data = (char *)malloc(datalen + 1))) {
			read(ctx->db_fd, (uint8_t *)data, datalen);
			data[datalen] = '\0';
		}
	}

	xoff += ntohs(ent.x);
	yoff += ntohs(ent.y);

	printf("BLOCK;%u;%u;%u;%u;%s\n", xoff, yoff,
			ntohs(ent.width), ntohs(ent.height),
			data ? data : "");

	if (data)
		free(data);

	if (children) {
		for (i = 0; i < ent.nc; i++) {
			// printf("child %d: %08x\n", i, children[i]);
			_lbs_print_layout_recursive(ctx, children[i], xoff, yoff);
		}

		free(children);
	}
}

int
lbs_pause(lbs_ctx_t *ctx)
{
	if (!ctx || !ctx->resumable)
		return -1;

	if (ctx->paused)
		return -1;

	ctx->paused = 1;

	close(ctx->lt_fd);
	close(ctx->db_fd);
	close(ctx->log_fd);

	return 0;
}

int
lbs_resume(lbs_ctx_t *ctx)
{
	if (!ctx || !ctx->resumable)
		return -1;

	if (!ctx->paused)
		return -1;

	ctx->lt_fd = _load_lbs_file(ctx->ltpath, ctx->oflag);
	ctx->db_fd = _load_lbs_file(ctx->dbpath, ctx->oflag);
	ctx->log_fd = _load_lbs_file(ctx->logpath, ctx->oflag);

	ctx->paused = 0;

	return 0;
}

int
lbs_print_frame(lbs_ctx_t *ctx, int idx)
{
	frame_t frame;

	lseek(ctx->log_fd, sizeof(lbs_file_t) + idx * sizeof(frame_t), SEEK_SET);
	read(ctx->log_fd, (uint8_t *)&frame, sizeof(frame_t));

	printf("FRAME;%u;%u\n", ntohl(frame.tv_sec), ntohl(frame.tv_usec));

	_lbs_print_layout_recursive(ctx, frame.root, 0, 0);

	return 0;
}

int
lbs_count_frames(lbs_ctx_t *ctx)
{
	size_t logfile_size = _get_filesize(ctx->log_fd);

	return (logfile_size - sizeof(lbs_file_t)) / sizeof(frame_t);
}
