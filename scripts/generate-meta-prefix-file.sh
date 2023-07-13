#!/bin/bash

if [[ -z $METADATA_PREFIXES_FILE_URI ]]; then 
  echo "Variable 'METADATA_PREFIXES_FILE_URI' not defined, using default metadata prefix file."
  exit  
fi

if [[ -z $METADATA_PREFIXES ]]; then 
  echo "No metadata prefixes provided, falling back to default metadata prefix file." 
  exit
fi

# build prefix file
echo $METADATA_PREFIXES >> $METADATA_PREFIXES_FILE_URI