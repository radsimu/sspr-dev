run scripts are in ro_data
unzip the two archives called "unzip_me_here" from ro_data/ and ro_data/batches/

-ExtractDerivations compiles ro_data/ro_derivations_compiled.txt - a list of nouns derived from verbs, that resemble events. Uses files from /ro_data/partial_ro_derivations and the posDictRoDiacr.txt

-AddConllFeatures adds features in the 5th column - uses the resource compiled by ExtractDerivations

-maltparser-1.9.0 - sspr is an altered version of maltparser. Contains an extra feature function called Attraction

-Dashboard is for running pipelines (add features - train - eval - show errors)