#!/usr/bin/env bash

test_description="Test handling of command line arguments"

. ./lib/sharness.sh
fake_install

test_expect_success "should print usage to stderr and exit non-zero for unknown argument" "
  test_expect_stderr_matching '^Usage: ' \
      test_expect_code 1 neo5j-home/bin/neo5j nonsense
"

test_expect_success "should print usage stdout and exit zero when help requested explicitly" "
  test_expect_stdout_matching '^Usage: ' neo5j-home/bin/neo5j help
"

test_expect_success "should include script name in usage" "
  test_expect_stdout_matching ' neo5j ' neo5j-home/bin/neo5j help
"

test_expect_success "should include --version in java args when --version given" "
  neo5j-home/bin/neo5j --version && test_expect_java_arg '--version'
"

test_expect_success "should include --version in java args when version given" "
  neo5j-home/bin/neo5j version && test_expect_java_arg '--version'
"

test_done
