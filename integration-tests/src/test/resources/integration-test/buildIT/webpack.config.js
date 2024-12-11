/**
 * The contents of this file are subject to the OpenMRS Public License
 * Version 1.0 (the "License"); you may not use this file except in
 * compliance with the License. You may obtain a copy of the License at
 * http://license.openmrs.org
 * Software distributed under the License is distributed on an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied. See the
 * License for the specific language governing rights and limitations
 * under the License.
 * Copyright (C) OpenMRS, LLC.  All Rights Reserved.
 */
// generated on 2016-07-18 using generator-openmrs-owa 0.3.0
'use strict';
const webpack = require('webpack');
const path = require('path');
const fs = require('fs');
const env = require('yargs').argv.mode;
const target = require('yargs').argv.target;

const UglifyPlugin = webpack.optimize.UglifyJsPlugin;
const CommonsChunkPlugin = webpack.optimize.CommonsChunkPlugin;
const DedupePlugin = webpack.optimize.DedupePlugin;

const HtmlWebpackPlugin = require('html-webpack-plugin');
const BrowserSyncPlugin = require('browser-sync-webpack-plugin');
const CopyWebpackPlugin = require('copy-webpack-plugin');
const WebpackOnBuildPlugin = require('on-build-webpack');
const ngAnnotatePlugin = require('ng-annotate-webpack-plugin');

const nodeModulesDir = path.resolve(__dirname, '../node_modules');

const THIS_APP_ID = 'test';

var plugins = [];
const nodeModules = {};

let outputFile = `.bundle`;
let outputPath;

var configJson;
let appEntryPoint;
let localOwaFolder;

let devtool;

var getConfig = function () {
    var config;

    try {
        // look for config file
        config = require('./config.json');
    } catch (err) {
        // create file with defaults if not found
        config = {
            'LOCAL_OWA_FOLDER': '/home/user/openmrs/openmrs-platform/',
            'APP_ENTRY_POINT': 'http://localhost:8080/openmrs/owa/test/index.html'
        };

        fs.writeFile('config.json', JSON.stringify(config));

    } finally {
        return config;
    }

}
var config = getConfig();

/** Minify for production */
if (env === 'production') {
    plugins.push(new ngAnnotatePlugin());
    plugins.push(new UglifyPlugin({
        output: {
            comments: false,
        },
        minimize: true,
        sourceMap: false,
        compress: {
            warnings: false
        }
    }));
    plugins.push(new DedupePlugin());
    outputFile = `${outputFile}.min.js`;
    outputPath = `${__dirname}/dist/`;
    plugins.push(new WebpackOnBuildPlugin(function (stats) {
        //create zip file
        var archiver = require('archiver');
        var output = fs.createWriteStream(THIS_APP_ID + '.zip');
        var archive = archiver('zip');

        output.on('close', function () {
            console.log('distributable has been zipped! size: ' + archive.pointer());
        });

        archive.on('error', function (err) {
            throw err;
        });

        archive.pipe(output);
        archive.bulk([
            {expand: true, cwd: 'dist/', src: ['**']}
        ]);
        archive.finalize();
    }))

} else if (env === 'deploy') {
    outputFile = `${outputFile}.js`;
    outputPath = `${config.LOCAL_OWA_FOLDER}${THIS_APP_ID}`;
    devtool = 'source-map';

} else if (env === 'dev') {
    outputFile = `${outputFile}.js`;
    outputPath = `${__dirname}/dist/`;
    devtool = 'source-map';
}

plugins.push(new BrowserSyncPlugin({
    proxy: {
        target: config.APP_ENTRY_POINT
    }
}));

plugins.push(new CommonsChunkPlugin("vendor", "vendor.bundle.js"));

plugins.push(new HtmlWebpackPlugin({
    template: './app/index.html',
    inject: 'body'
}));

plugins.push(new CopyWebpackPlugin([{
    from: './app/manifest.webapp'
}]));

plugins.push(new CopyWebpackPlugin([{
    from: './app/img/omrs-button.png',
    to: 'img/omrs-button.png'
}]));

plugins.push(new ngAnnotatePlugin({
    add: true,
    map: false
}));

var webpackConfig = {
    quiet: false,
    entry: {
        app: `${__dirname}/app/js/test.js`,
        css: `${__dirname}/app/css/test.css`,
        vendor: [
            'angular'
        ]
    },
    devtool: devtool,
    target,
    output: {
        path: outputPath,
        filename: '[name]' + outputFile,
    },
    module: {
        loaders: [{
            test: /\.jsx?$/,
            loader: 'babel-loader',
            exclude: /node_modules/,
            query: {
                presets: ['es2015'],
                cacheDirectory: true
            }
        }, {
            test: /\.css$/,
            loader: 'style-loader!css-loader'
        }, {
            test: /\.(png|jpg|jpeg|gif|svg)$/,
            loader: 'url'
        }, {
            test: /\.html$/,
            loader: 'html'
        }],
    },
    resolve: {
        root: path.resolve('./src'),
        extensions: ['', '.js'],
    },
    plugins,
    externals: nodeModules,
};

module.exports = webpackConfig;
