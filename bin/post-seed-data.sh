#!/bin/bash

# Either get server IP:port from CLI or default to localhost
input_ip=$1
server_ip="${input_ip:=http://localhost:8080}/api"

# Go to the directory the files are located
cd $(dirname "$0")/../src/test/resources || exit 1

# Create the map to hold key=fileanmes value=collection urls
declare -A collections

# Loop over files, upload to REST API, and add URLs to the collections map
collection_title=$(curl -X POST -H "Content-Type: application/json" -H "Authorization: Bearer $2" --data '{"title":"test"}' "${server_ip}/collections" | jq -r .title)

for jsonld in "simple.input.jsonld" "scidata_nmr_abbreviated.input.jsonld" "studtite.jsonld"
do
    model_uuid=$(curl -X POST "${server_ip}/collections/${collection_title}/models" -H "Authorization: Bearer $2" -H "Content-Type: application/json" -d @${jsonld} | jq -r .uuid)
    name=$(echo ${jsonld} | cut -d'.' -f 1)
    collections["${name}"]="${server_ip}/collections/${collection_title}/models/${model_uuid}"
done

# Error check the collection and models
if [ -z $collection_title ]
then
    echo "ERROR: No collection title, maybe unable to upload?"
    exit 1
fi

if [ -z $model_uuid ]
then
    echo "ERROR: No model UUID, maybe unable to upload?"
    exit 1
fi

# Print out the results
for name in "${!collections[@]}"
do
    printf "%s url:\n" "$name"
    printf "    %s\n\n" "${collections[$name]}"
done
