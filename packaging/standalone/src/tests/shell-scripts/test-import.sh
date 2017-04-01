#!/usr/bin/env bash

test_description="Test running neo5j-import"

. ./lib/sharness.sh
fake_install

test_expect_success "should invoke import main class" "
  neo5j-home/bin/neo5j-import || true &&
  test_expect_java_arg 'org.neo5j.tooling.ImportTool'
"

test_done
