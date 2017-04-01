#!/usr/bin/env bash

test_description="Test differences running the arbiter"

. ./lib/sharness.sh
fake_install

set_config 'dbms.mode' 'ARBITER' neo5j.conf
touch "$(neo5j_home)/lib/neo5j-server-enterprise-0.0.0.jar"

test_expect_success "should start successfully" "
  run_console >neo5j.stdout
"

test_expect_success "should invoke arbiter main class" "
  test_expect_java_arg 'org.neo5j.server.enterprise.ArbiterEntryPoint'
"

test_expect_success "should print a specific startup message" "
  test_expect_stdout_matching 'This instance is now joining the cluster.' run_daemon
"

test_done
