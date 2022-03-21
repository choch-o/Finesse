#include <assert.h>
#include <unistd.h>
#include <stdlib.h>
#include <string.h>
#include <stddef.h>
#include <arpa/inet.h>

#include "lbs.h"

#undef get16bits
#if (defined(__GNUC__) && defined(__i386__)) || defined(__WATCOMC__) \
	|| defined(_MSC_VER) || defined (__BORLANDC__) || defined (__TURBOC__)
#define get16bits(d) (*((const uint16_t *) (d)))
#endif

#if !defined (get16bits)
#define get16bits(d) ((((uint32_t)(((const uint8_t *)(d))[1])) << 8)\
		+(uint32_t)(((const uint8_t *)(d))[0]) )
#endif

/* SuperFastHash */
uint32_t
hash32(const char *data, int len) {
	register uint32_t hash = len, tmp;
	int rem;

	if (len <= 0 || data == NULL) return 0;

	rem = len & 3;
	len >>= 2;

	/* Main loop */
	for (;len > 0; len--) {
		hash  += get16bits (data);
		tmp    = (get16bits (data+2) << 11) ^ hash;
		hash   = (hash << 16) ^ tmp;
		data  += 2*sizeof (uint16_t);
		hash  += hash >> 11;
	}

	/* Handle end cases */
	switch (rem) {
		case 3: hash += get16bits (data);
				hash ^= hash << 16;
				hash ^= ((signed char)data[sizeof (uint16_t)]) << 18;
				hash += hash >> 11;
				break;
		case 2: hash += get16bits (data);
				hash ^= hash << 11;
				hash += hash >> 17;
				break;
		case 1: hash += (signed char)*data;
				hash ^= hash << 10;
				hash += hash >> 1;
	}

	/* Force "avalanching" of final 127 bits */
	hash ^= hash << 3;
	hash += hash >> 5;
	hash ^= hash << 4;
	hash += hash >> 17;
	hash ^= hash << 25;
	hash += hash >> 6;

	return htonl(hash);
}

size_t
_lbs_hash_lookup(lbs_ctx_t *lctx, hash_t hash)
{
	return (size_t)kvs_search(lctx->lt, hash);
}

int
_lbs_write(int fd, uint8_t *buffer, size_t len)
{
	size_t written;
	int ret;

	for (written = 0; written < len; written += ret)
		if ((ret = write(fd, &buffer[written], len - written)) <= 0)
			/* TODO: Handle write failure. file corruption */
			return -1;

	return written;
}

int
_lbs_append_db(lbs_ctx_t *lctx, hash_t key, uint8_t *data, size_t len)
{
	lookup_ent_t lt_ent;
	off_t pos;

	pos = lseek(lctx->db_fd, 0, SEEK_END);

	/* store hash to in-memory hash table */
	kvs_insert(lctx->lt, key, (_value_t)pos);

	/* store hash to lookup table on the disk */
	lt_ent.hash = key;
	lt_ent.offset = htonl((uint32_t)pos);
	_lbs_write(lctx->lt_fd, (uint8_t *)&lt_ent, sizeof(lookup_ent_t));

	/* store content to database on the disk */
	_lbs_write(lctx->db_fd, data, len);

	return 0;
}

int
_lbs_commit_layout(lbs_ctx_t *lctx, layout_elm_t *elm, hash_t content_hash)
{
	int nc = 0, i = 0, ret;
	size_t entsize;
	layout_ent_t *ent;
	hash_t hash;

	if (!lctx || !elm)
		return -1;

	if (elm->child) {
		layout_elm_t *child;
		for (child = elm->child;
			 child;
			 child = child->sibling)
			nc++;
	}

	entsize = sizeof(layout_ent_t) + nc * sizeof(hash_t);
	if (!(ent = (layout_ent_t *)calloc(entsize, 1)))
		return -1;

	ent->x = htons(elm->x);
	ent->y = htons(elm->y);
	ent->nc = (uint8_t)nc;
	ent->width = htons(elm->width);
	ent->height = htons(elm->height);

	elm->hash = hash32((const char *)ent, offsetof(struct _layout_ent_t, content));
	
	if (nc) {
		layout_elm_t *child;
		for (child = elm->child;
			 child;
			 child = child->sibling) {
			ent->children[i++] = child->hash;
			elm->hash ^= child->hash;
		}
	}

	ent->content = content_hash;

	elm->hash ^= content_hash;

	// printf("offsetof: %d\n", offsetof(struct _layout_ent_t, content));
	// printf("nc: %d, hash: 0x%08X, content_hash = 0x%08X\n", nc, elm->hash, content_hash);

	if (_lbs_hash_lookup(lctx, elm->hash)) {
		/* nothing to do on DB hit */
		free(ent);
		return 0;
	}

	if (_lbs_append_db(lctx, elm->hash, (uint8_t *)ent, entsize)) {
		free(ent);
		return -1;
	}

	free(ent);

	return 0;
}

int
_lbs_commit_content(lbs_ctx_t *lctx, layout_elm_t *elm, hash_t *hash_out)
{
	size_t contsize;
	content_ent_t *cont;
	hash_t hash;

	if (!lctx || !elm)
		return -1;

	contsize = sizeof(content_ent_t) + elm->extralen;
	if (!(cont = (content_ent_t *)calloc(contsize, 1))) {
		return -1;
	}

	cont->datalen = htons(elm->extralen);

	if (elm->extralen > 0)
		memcpy(cont->data, elm->extra, elm->extralen);

	hash = hash32((const char *)cont, contsize);

	// printf("extra: %s, hash: 0s%08X\n", elm->extra, hash);

	if (_lbs_hash_lookup(lctx, hash)) {
		/* nothing to do on DB hit */
		*hash_out = hash;
		free(cont);
		return 0;
	}

	if (_lbs_append_db(lctx, hash, (uint8_t *)cont, contsize)) {
		free(cont);
		return -1;
	}

	*hash_out = hash;

	free(cont);

	return 0;
}

void
_lbs_commit_elm(lbs_ctx_t *lctx, layout_elm_t *elm)
{
	hash_t hash;

	if (!lctx || !elm)
		return;

	_lbs_commit_content(lctx, elm, &hash);
	_lbs_commit_layout(lctx, elm, hash);

	return;
}

void
_lbs_dfs_elm(lbs_ctx_t *lctx, layout_elm_t *root,
		void (*func)(lbs_ctx_t *, layout_elm_t *))
{
	layout_elm_t *elm, *next;

	int stacksize = 0;
	bool pop = false;
	layout_elm_t *stack[LBS_MAX_TREE_DEPTH];

	if (!root)
		return;

	/* DFS layout tree */
	for (elm = root; elm; elm = next) {
		if (elm->child && !pop) {
			stack[stacksize++] = elm; /* push */
			next = elm->child;
			pop = false;
			continue;
		}

		if (elm->sibling) {
			next = elm->sibling;
			pop = false;
		} else if (stacksize > 0) {
			next = stack[--stacksize]; /* pop */
			pop = true;
		} else
			next = NULL;

		/* do work */
		func(lctx, elm);
	}
}

void
_lbs_commit_elm_recursive(lbs_ctx_t *lctx, layout_elm_t *root)
{
	_lbs_dfs_elm(lctx, root, _lbs_commit_elm);
}
