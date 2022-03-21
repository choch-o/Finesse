#include <stdio.h>

#include "lbs.h"

int
main(int argc, char **argv)
{
	lbs_ctx_t lctx;
	layout_elm_t *elm[5];
	int i;

	if (argc != 4)
		return -1;

	if (lbs_init_ctx(&lctx, argv[1], argv[2], argv[3], true) < 0)
		return -1;

	int numframes = lbs_count_frames(&lctx);

	fprintf(stderr, "%d frames\n", numframes);

	for (i = 0; i < numframes; i++)
		lbs_print_frame(&lctx, i);
	
	lbs_destroy_ctx(&lctx);

	return 0;
}
