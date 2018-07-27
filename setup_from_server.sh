cd omnitrack_visualization_core && npm install
node_modules/.bin/webpack
cd ..
cp -a omnitrack_visualization_core/built/. ./app/src/main/assets/web/visualization/
