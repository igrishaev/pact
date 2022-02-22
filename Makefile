
.PHONY: test-all
test-all: test test-js

.PHONY: test
test:
	lein test


.PHONY: release
release:
	lein release


test-js:
	lein with-profile +cljs cljsbuild once
	node target/tests.js
