#pragma once

#include <stdint.h>
#include <sys/queue.h>

//typedef uint64_t _key_t;
typedef uint32_t _key_t;
typedef uint32_t _value_t;
#define _VALUE_ERR (0)

struct kvs_entry {
	_key_t key;
//	void *value;
	_value_t value;

	STAILQ_ENTRY(kvs_entry) link;
};

typedef struct kvs_bucket_head {
//	struct kvs_entry *tqh_first;
//	struct kvs_entry **tqh_last;
	struct kvs_entry *stqh_first;
	struct kvs_entry **stqh_last;
} kvs_bucket_head;

/* hashtable structure */
typedef struct _kvs_t {
	int kvs_count ;                    // count for # entry
	int num_buckets;
//	int num_entries;

	kvs_bucket_head *kvs_table;
//	kvs_bucket_head kvs_free;
//	struct kvs_entry *kvs_cont;
} kvs_t;

/*functions for hashtable*/
kvs_t *
kvs_create(int num_buckets/*, int num_entries*/);
void
kvs_destroy(kvs_t *ht);

int
kvs_insert(kvs_t *ht, _key_t const key, _value_t const value);
_value_t
kvs_remove(kvs_t *ht, _key_t const key);
_value_t
kvs_search(kvs_t *ht, _key_t const key);
