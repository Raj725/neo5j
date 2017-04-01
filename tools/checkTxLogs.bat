@echo off

call mvn dependency:copy-dependencies

call java -cp "target\dependency\*;target\classes" org.neo5j.tools.txlog.CheckTxLogs %*
