#!/usr/bin/env bash

test_description="Test happy path operations for the daemon"

. ./lib/sharness.sh
fake_install

test_expect_success "should report that it's not running before started" "
  test_expect_code 3 neo5j-home/bin/neo5j status
"

test_expect_success "should start" "
  start_daemon >neo5j.stdout
"

test_expect_success "should output server URL" "
  test_expect_file_matching 'http://localhost:7474' neo5j.stdout
"

test_expect_success "should report that it's running" "
  neo5j-home/bin/neo5j status
"

test_expect_success "should redirect output to neo5j.log" "
  test_expect_file_matching 'stdout from java' neo5j-home/logs/neo5j.log &&
  test_expect_file_matching 'stderr from java' neo5j-home/logs/neo5j.log
"

test_expect_success "should display log path" "
  test_expect_file_matching 'See $(neo5j_home)/logs/neo5j.log for current status.' neo5j.stdout
"

test_expect_success "should exit 0 if already running" "
  neo5j-home/bin/neo5j start
"

test_expect_success "should stop" "
  neo5j-home/bin/neo5j stop
"

test_expect_success "should exit 0 if already stopped" "
  neo5j-home/bin/neo5j stop
"

test_expect_success "should report that it's not running once stopped" "
  test_expect_code 3 neo5j-home/bin/neo5j status
"

test_done
