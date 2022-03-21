#include <stdio.h>

#include "lbs.h"

int
main(int argc, char **argv)
{
	lbs_ctx_t lctx;
	layout_elm_t *elm[5];

#define EXT1 "extra 1"
#define EXT2 "extra 2"
#define EXT3 "extra 3"
#define EXT4 "extra 4"
#define EXT5 "extra 5"

	lbs_init_ctx(&lctx, "0.lt.lbs", "0.db.lbs", "0.log.lbs", false);
	
	elm[0] = lbs_new_layout_elm(0, 0, 0, 0, EXT1, sizeof(EXT1));
	elm[1] = lbs_new_layout_elm(0, 0, 0, 0, EXT2, sizeof(EXT2));
	elm[2] = lbs_new_layout_elm(0, 0, 0, 0, EXT3, sizeof(EXT3));
	elm[3] = lbs_new_layout_elm(0, 0, 0, 0, EXT4, sizeof(EXT4));
	elm[4] = lbs_new_layout_elm(0, 0, 0, 0, EXT5, sizeof(EXT5));

	lbs_add_child(elm[0], elm[1]);
	lbs_add_child(elm[0], elm[2]);
	lbs_add_child(elm[1], elm[3]);
	lbs_add_child(elm[1], elm[4]);

	lbs_commit_frame(&lctx, elm[0], NULL);

	lbs_destroy_ctx(&lctx);

	return 0;
}
