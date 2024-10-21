generate-index:
	clj -X exp.index/generate-index

watch:
	npx shadow-cljs watch :app

prepare-release: generate-index
	rm -rf release ;\
	mkdir -p release ;\
	cp public/*.html release/ ;\
	npx shadow-cljs release :app --config-merge '{:output-dir "release/js/compiled"}'
