import template from './main.html';
import controller from './main.controller.js';

let mainComponent = {
    restrict: 'E',
    bindings: {},
    template: template,
    controller: controller,
    controllerAs: 'vm'
};

export default mainComponent;
