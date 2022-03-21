#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <assert.h>
#include <sys/queue.h>
//#include <android/log.h>
#include "key_value_store.h"

/*----------------------------------------------------------------------------*/
kvs_t * 
kvs_create(int num_buckets/*, int num_entries*/)
{
	int i;
	kvs_t *ht = calloc(1, sizeof(kvs_t));
	if (!ht)
		return NULL;

	ht->num_buckets = num_buckets;
//	ht->num_entries = num_entries;

	/* init the tables */
	if (!(ht->kvs_table = calloc(num_buckets, sizeof(kvs_bucket_head)))) {
		free(ht);
		return NULL;
	}
	for (i = 0; i < num_buckets; i++)
		STAILQ_INIT(&ht->kvs_table[i]);

//	if (!(ht->kvs_cont = calloc(num_entries, sizeof(struct kvs_entry)))) {
//		free(ht->kvs_table);
//		free(ht);
//		return NULL;
//	}
//	STAILQ_INIT(&ht->kvs_free);
//	for (i = 0; i < num_entries; i++)
//		STAILQ_INSERT_TAIL(&ht->kvs_free, &ht->kvs_cont[i], link);

	return ht;
}
/*----------------------------------------------------------------------------*/
void
kvs_destroy(kvs_t *ht) {
	struct kvs_entry *walk;
	kvs_bucket_head *head;
	int i;
	for (i = 0; i < ht->num_buckets; i++) {
		head = &ht->kvs_table[i];
		STAILQ_FOREACH(walk, head, link) {
			STAILQ_REMOVE(head, walk, kvs_entry, link);
			free(walk);
			ht->kvs_count--;
		}
	}
	free(ht->kvs_table);
//	free(ht->kvs_cont);
	free(ht);	
}
/*----------------------------------------------------------------------------*/
int 
kvs_insert(kvs_t *ht, _key_t const key, _value_t const value)
{
	/* create an entry*/ 
	int idx;

//	assert(value);

	assert(ht);
//	assert(ht->kvs_count <= ht->num_entries);

	if (kvs_search(ht, key))
		return -1;

	idx = key % ht->num_buckets;
	assert(idx >=0 && idx < ht->num_buckets);

	/* get a container */
	struct kvs_entry *ent;
//	if (!(ent = STAILQ_FIRST(&ht->kvs_free)))
//		return -1;
//	STAILQ_REMOVE(&ht->kvs_free, ent, link);
	if (!(ent = calloc(1, sizeof(struct kvs_entry))))
		return -1;

	/* put the value to the container */
	ent->key = key;
	ent->value = value;

	/* insert the container */
	STAILQ_INSERT_TAIL(&ht->kvs_table[idx], ent, link);
	ht->kvs_count++;

	// TRACE_DBG("%lX inserted to 0x%p\n", key, ht);

	return 0;
}
/*----------------------------------------------------------------------------*/
_value_t
kvs_remove(kvs_t *ht, _key_t const key)
{
	struct kvs_entry *walk;
	kvs_bucket_head *head;

	head = &ht->kvs_table[key % ht->num_buckets];
	STAILQ_FOREACH(walk, head, link) {
		if (key == walk->key) {
			_value_t value;
//			TAILQ_REMOVE(head, walk, link);
			STAILQ_REMOVE(head, walk, kvs_entry, link);
//			STAILQ_INSERT_TAIL(&ht->kvs_free, walk, link);
			value = walk->value;
			free(walk);
			ht->kvs_count--;

			// TRACE_DBG("%lX removed from 0x%p\n", key, ht);
			return value;
		}
	}

	return _VALUE_ERR;
}	
/*----------------------------------------------------------------------------*/
_value_t
kvs_search(kvs_t *ht, _key_t const key)
{
//	 TRACE_DBG("look for %lX from 0x%p..\n", key, ht);

	struct kvs_entry *walk;
	kvs_bucket_head *head;

	head = &ht->kvs_table[key % ht->num_buckets];
	STAILQ_FOREACH(walk, head, link) {
		if (key == walk->key)
			return walk->value;
	}

	return _VALUE_ERR;
}
/*----------------------------------------------------------------------------*/
