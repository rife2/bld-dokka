#!/bin/bash

main=org.jetbrains.dokka.MainKt
new=/tmp/checkcliargs-new
old=/tmp/checkcliargs-old

cd examples
./bld javadoc
cd ..

java -cp "examples/lib/bld/bld-dokka/.sandbox/*" $main -h >$new
java -cp "examples/lib/bld/.sandbox/dokka-html/*" $main -h >$old

diff $old $new

java -cp "lib/provided/*" $main -sourceSet -h >$new
java -cp "examples/lib/bld/.sandbox/dokka-html/*" $main -sourceSet -h >$old

diff $old $new
