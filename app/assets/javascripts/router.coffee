define ['jQuery', 'Underscore', 'Backbone', 'views/AppView', 'views/RegisterView'],
($, _, Backbone, AppView, RegisterView) ->
    class AppRouter extends Backbone.Router
        routes:
            '': 'indexAction'
            'register': 'registerAction'
            '*default': 'defaultAction'
        
        indexAction: ->
            RegisterView.undelegateEvents()
            AppView.render()
            AppView.delegateEvents()
        
        registerAction: ->
            AppView.undelegateEvents()
            RegisterView.render()
            RegisterView.delegateEvents()
        
        defaultAction: ->
            console.log 'No route!'
        
    
    initialize = ->
        app_router = new AppRouter
        Backbone.history.start()
    
    {
        initialize: initialize
    }