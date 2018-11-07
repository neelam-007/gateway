#!/bin/bash
FILES=$(find . -name *.class)
for FILE in $FILES; do
  DEST=$(sed "s%build/classes%src/main/java%" <<<"$FILE")
  DEST=$(sed "s%build/test-classes%src/test/java%" <<<"$DEST")
  if [[ $DEST != *"/build/"* ]]; then
    cp $FILE $DEST
  fi
done