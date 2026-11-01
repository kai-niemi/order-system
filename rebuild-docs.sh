#!/usr/bin/env bash

bash export-schemas.sh
bash prepare-docs.sh
zensical build
zensical serve