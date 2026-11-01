#!/bin/bash

main=org.jetbrains.dokka.MainKt
lib=examples/lib/.sandbox/bld-dokka/default-plugin

java -cp "$lib/*" $main -h |\
grep "    -" |\
sed -e "s/^    -/-/" -e "s/ \[.*//" -e "s/ ->.*//" -e '/help/d' |\
sort > "src/test/resources/dokka-args.txt"

java -cp "$lib/*" $main -sourceSet -h |\
grep "    -" |\
sed -e "s/^    -/-/" -e "s/ \[.*//" -e "s/ ->.*//" -e '/help/d' -e '/includeNonPublic/d' |\
sort > "src/test/resources/dokka-sourceset-args.txt"
