define ['marionette', 'jquery', 'views/MenuView', 'models/MenuItem'],
(Marionette, $, MenuView, MenuItem) ->
    class RegisterView extends Marionette.ItemView
        
        template: '#tmpl-user-signup'
        
        events:
            'submit form': 'submit'

        menuitem: new MenuItem {
            title: 'Registro'
            tag: 'register'
        }
        
        initialize: ->
            MenuView.leftitems.add @menuitem

        submit: (event) ->
            $.ajax(event.target.action, {
                data: $(event.target).serialize()
                processData: false
                type: 'POST'
            }).done( (data) =>
                alert 'Registrado correctamente'
            )
            false
    
    new RegisterView