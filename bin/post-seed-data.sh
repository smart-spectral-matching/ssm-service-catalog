#!/bin/bash

# Either get server IP:port from CLI or default to localhost
input_ip=$1
server_ip="${input_ip:=http://localhost:8080}/api"

# Go to the directory the files are located
cd $(dirname "$0")/../src/test/resources || exit 1

# Create the map to hold key=fileanmes value=dataset urls
declare -A datasets

# Loop over files, upload to REST API, and add URLs to the datasets map
dataset_title=$(curl -X POST -H "Content-Type: application/json" --data '{"title":"test"}' "${server_ip}/datasets" | jq -r .title)
for jsonld in "simple.input.jsonld" "scidata_nmr_abbreviated.input.jsonld" "studtite.jsonld"
do
    model_uuid=$(curl -X POST "${server_ip}/datasets/${dataset_title}/models" -H "Content-Type: application/json" -d @${jsonld} | jq -r .uuid)
    name=$(echo ${jsonld} | cut -d'.' -f 1)
    datasets["${name}"]="${server_ip}/datasets/${dataset_title}/models/${model_uuid}"
done

# Error check the dataset and models
if [ -z $dataset_title ]
then
    echo "ERROR: No dataset title, maybe unable to upload?"
    exit 1
fi

if [ -z $model_uuid ]
then
    echo "ERROR: No model UUID, maybe unable to upload?"
    exit 1
fi

# Print out the results
for name in "${!datasets[@]}"
do
    printf "%s url:\n" "$name"
    printf "    %s\n\n" "${datasets[$name]}"
done
