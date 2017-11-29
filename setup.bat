@echo Generate properties with placeholder contents.
COPY /-Y .gradle.properties.example gradle.properties
COPY /-Y .keystore.properties.example keystore.properties

@echo update git submodules.
git submodule update --init --recursive
git submodule foreach "(git checkout master; git pull)&"

@echo setup visualization javascripts.
cd omnitrack_visualization_core
call npm install

@echo pack the javascript files. This process may take about a minute...

call node_modules\.bin\webpack.cmd
cd ..
xcopy /E "omnitrack_visualization_core/built" "app/src/main/assets/web/visualization"

@echo Now you are ready to build the OmniTrack application. Enjoy!
