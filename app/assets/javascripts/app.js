define([
  'jQuery',
  'Underscore',
  'Backbone',
  'router', // Request router.js
  'libs/webshims/polyfiller'
], function($, _, Backbone, Router){
  var initialize = function(){
    // Pass in our Router module and call it's initialize function
    Router.initialize();
  }
  
  $.webshims.setOptions({
      waitReady: false,
      basePath: '/assets/javascripts/libs/webshims/shims/'
  });
  
  $.webshims.polyfill();
  
  return {
    initialize: initialize
  };
});