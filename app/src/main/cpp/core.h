#pragma once

void
_lbs_dfs_elm(lbs_ctx_t *lctx, layout_elm_t *root,
		void (*func)(lbs_ctx_t *, layout_elm_t *));

void
_lbs_commit_elm_recursive(lbs_ctx_t *lctx, layout_elm_t *root);

int
_lbs_write(int fd, uint8_t *buffer, size_t len);

