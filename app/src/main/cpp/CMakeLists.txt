cmake_minimum_required(VERSION 3.4.1)

# project(lbs)

add_library(
	lbs SHARED

	api.c
	core.c
	key_value_store.c
	)

# set_target_properties(lbs PROPERTIES CMAKE_CXX_FLAGS "${CMAKE_CXX_FLAGS} -g -ggdb")

if (ANDROID)
	add_library(
		lbs_android SHARED

		jni.c
		)
	target_link_libraries(
		lbs_android

		lbs
		log
		)
	target_link_libraries(
		lbs

		log
	)
else()
	add_executable(
		reader

		reader.c
		)

	target_link_libraries(
		reader

		lbs
		)
	add_executable(
		test

		test.c
		)

	target_link_libraries(
		test

		lbs
		)
endif()
