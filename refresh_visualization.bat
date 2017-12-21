cd omnitrack_visualization_core
call node_modules\.bin\webpack.cmd
cd ..

xcopy /E "omnitrack_visualization_core/built" "app/src/main/assets/web/visualization"
