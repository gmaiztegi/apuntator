define ['use!marionette'], (Marionette) ->
    app = new Marionette.Application
    app.addRegions
        menu: '#menu'
        main: '#content'
    
    app.main.on 'view:closed', (view) ->
    	if view.menuitem then view.menuitem.set 'active', false

    app.main.on 'view:show', (view) ->
    	if view.menuitem then view.menuitem.set 'active', true

    app