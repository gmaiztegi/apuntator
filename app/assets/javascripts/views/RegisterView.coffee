define ['backbone', 'jquery', 'handlebars', 'text!templates/user/signup.html'],
(Backbone, $, Handlebars, form) ->
    class RegisterView extends Backbone.View
        el: $('#content')
        
        formTemplate: Handlebars.compile form
        
        events:
            'submit form': 'submit'
        
        render: ->
            @$el.html @formTemplate()
            @
        
        submit: (event) ->
            $.ajax(event.target.action, {
                data: $(event.target).serialize()
                processData: false
                type: 'POST'
            }).done( (data) =>
                console.log(data)
                alert 'Registrado correctamente'
            )
            false
    
    new RegisterView