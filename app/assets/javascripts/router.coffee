define ['use!marionette', 'ApuntatorApp', 'views/FilesView', 'views/RegisterView'],
(Marionette, App, FilesView, RegisterView) ->
    class Router extends Marionette.AppRouter
        appRoutes:
            '': 'filesAction'
            'files': 'filesAction'
            'register': 'registerAction'
            '*default': 'defaultAction'
        
        controller:
            filesAction: ->
                App.main.show(FilesView)

            registerAction: ->
                App.main.show(RegisterView)

            defaultAction: ->
