define ['use!backbone'], (Backbone) ->
    class User extends Backbone.Model
        urlRoot: 'api/users'