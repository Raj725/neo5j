#!/usr/bin/env bash

test_description="Test running neo5j-backup"

. ./lib/sharness.sh
fake_install

test_expect_success "should invoke backup main class" "
  neo5j-home/bin/neo5j-backup || true &&
  test_expect_java_arg 'org.neo5j.backup.BackupTool'
"

test_done
