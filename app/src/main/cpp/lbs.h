/**
 * Layout Bounds Stream. June 2020, Donghwi Kim, dhkim09a@gmail.com
 *
 * Space-efficient format of storing layout bounds stream.
 **/

#pragma once

#include <stdint.h>
#include <stdbool.h>
#include <sys/time.h>
#include <sys/queue.h>

#include "key_value_store.h"

typedef uint16_t word_t;
typedef uint32_t hash_t;

/* File format */

#define LBS_MAX_TREE_DEPTH 128
#define LBS_MAX_SIBLINGS   128

typedef struct _lookup_ent_t {
	hash_t hash; /* 32-bit hash */
	uint32_t offset;
} __attribute__((packed)) lookup_ent_t;

typedef struct _layout_ent_t {
#if defined(__BYTE_ORDER) && __BYTE_ORDER == __BIG_ENDIAN || \
    defined(__BIG_ENDIAN__) || \
    defined(__ARMEB__) || \
    defined(__THUMBEB__) || \
    defined(__AARCH64EB__) || \
    defined(_MIBSEB) || defined(__MIBSEB) || defined(__MIBSEB__)
// It's a big-endian target architecture
	uint32_t x:  12, /* relative x from the parent. (0 ~ 4095) */
			 y:  12, /* relative y from the parent. (0 ~ 4095) */
			 nc: 8;  /* number of children (0 ~ 255) */
#elif defined(__BYTE_ORDER) && __BYTE_ORDER == __LITTLE_ENDIAN || \
    defined(__LITTLE_ENDIAN__) || \
    defined(__ARMEL__) || \
    defined(__THUMBEL__) || \
    defined(__AARCH64EL__) || \
    defined(_MIPSEL) || defined(__MIPSEL) || defined(__MIPSEL__)
// It's a little-endian target architecture
	uint32_t nc: 8,  /* number of children (0 ~ 255) */
			 y:  12, /* relative y from the parent. (0 ~ 4095) */
			 x:  12; /* relative x from the parent. (0 ~ 4095) */
#else
#error "I don't know what architecture this is!"
#endif
	uint16_t width;
	uint16_t height;

	// DO NOT MODIFY BELOW.
	// All hash values MUST be below this line.
	hash_t content;
	hash_t children[0];
} __attribute__((packed)) layout_ent_t;

typedef struct _content_ent_t {
	uint16_t datalen; /* data should be [0 ~ 65535] */
	char data[0];
} __attribute__((packed)) content_ent_t;

typedef struct _layout_frame_t {
	// struct timeval tv; /* be aware of endianness. htonl(tv.usec) */
	uint32_t tv_sec;
	uint32_t tv_usec;
	hash_t root;
} __attribute__((packed)) frame_t;

typedef struct _lbs_file_t {
	uint32_t uuid;
	uint8_t type;
	uint8_t reserved[3]; /* for 32-bit alignment (performance) */
	union {
		/* lookup table file consists of lookup entries */
		lookup_ent_t entries[0];
		/* database file consists of layout_ent_t and content_ent_t */
		uint8_t data[0];
		/* log file consists of layout frames */
		frame_t frames[0];
	};
} __attribute__((packed)) lbs_file_t;

/* API */

typedef struct _layout_elm_t {
	int x;
	int y;
	int width;
	int height;

	hash_t hash;

	struct _layout_elm_t *child;
	struct _layout_elm_t *sibling;

	int extralen;
	char extra[0];
} layout_elm_t;

typedef struct _lbs_ctx_t {
	int lt_fd; /* lookup table fd */
	int db_fd; /* database fd */
	int log_fd; /* */

	int resumable: 1,
		paused: 1;
	int oflag;
	char ltpath[128];
	char dbpath[128];
	char logpath[128];

	kvs_t *lt;
} lbs_ctx_t;

layout_elm_t *
lbs_new_layout_elm(int x, int y, int width, int height, const char *extra, int extralen);

void
lbs_del_layout_elm(layout_elm_t *elm);

void
lbs_del_layout_recursive(layout_elm_t *root);

int
lbs_add_child(layout_elm_t *parent, layout_elm_t *child);

int
lbs_commit_frame(lbs_ctx_t *ctx, layout_elm_t *root, struct timeval *tv);

int
lbs_init_ctx(lbs_ctx_t *ctx, const char *ltpath, const char *dbpath, const char *logpath, bool rdonly);

int
lbs_destroy_ctx(lbs_ctx_t *ctx);

int
lbs_pause(lbs_ctx_t *ctx);

int
lbs_resume(lbs_ctx_t *ctx);

int
lbs_print_frame(lbs_ctx_t *ctx, int idx);

int
lbs_count_frames(lbs_ctx_t *ctx);
