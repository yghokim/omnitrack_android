const path = require('path');
const UglifyJsPlugin = require('uglifyjs-webpack-plugin')

module.exports = {
  entry: './visualization/visualization.ts',
  plugins: [ new UglifyJsPlugin() ],
  mode: 'production',
  module: {
    rules: [
      {
        test: /\.tsx?$/,
        use: 'ts-loader',
        exclude: /node_modules/
      }
    ]
  },
  resolve: {
    extensions: [ '.tsx', '.ts', '.js' ]
  },
  output: {
    libraryTarget: 'var',
    library: 'OTVis',
    filename: 'visualization.js',
    path: path.resolve(__dirname, 'built')
  }
  
};
