import angular from 'angular';
import mainComponent from './main.component.js';

let TESTModule = angular.module('TEST', [])
    .component('main', mainComponent);

export default TESTModule;
