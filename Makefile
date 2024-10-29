generate-index:
	clj -X exp.index/generate-index

watch:
	npx shadow-cljs watch :app

prepare-release: generate-index
	rm -rf docs ;\
	mkdir -p docs ;\
	cp public/*.html docs/ ;\
	npx shadow-cljs release :app --config-merge '{:output-dir "docs/js/compiled"}'
