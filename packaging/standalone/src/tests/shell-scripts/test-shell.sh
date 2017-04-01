#!/usr/bin/env bash

test_description="Test shell argument handling."

. ./lib/sharness.sh
fake_install

test_expect_success "should invoke shell main class" "
  neo5j-home/bin/neo5j-shell || true &&
  test_expect_java_arg 'org.neo5j.shell.StartClient'
"

test_done
