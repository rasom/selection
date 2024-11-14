watch: generate-index install-deps
	npx shadow-cljs watch :app

generate-index:
	mkdir -p public ;\
	clj -X exp.index/generate-index

install-deps:
	npm install

prepare-release: generate-index install-deps
	rm -rf docs ;\
	mkdir -p docs ;\
	cp public/*.html docs/ ;\
	npx shadow-cljs release :app --config-merge '{:output-dir "docs/js/compiled"}'

shell:
	nix-shell
