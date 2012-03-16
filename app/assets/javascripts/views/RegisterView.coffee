define ['use!marionette', 'jquery'],
(Marionette, $) ->
    class RegisterView extends Marionette.ItemView
        
        template: '#tmpl-user-signup'
        
        events:
            'submit form': 'submit'
        
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