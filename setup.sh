cp -i .keystore.properties.example keystore.properties
cp -i .omnitrackBuildConfig.json.example omnitrackBuildConfig.json

git submodule update --init --recursive
git submodule foreach "(git checkout master; git pull)&"
cd omnitrack_visualization_core && npm install
node_modules/.bin/webpack
cd ..
cp -a omnitrack_visualization_core/built/. ./app/src/main/assets/web/visualization/