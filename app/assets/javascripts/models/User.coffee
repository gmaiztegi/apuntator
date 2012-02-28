define ['Backbone'], (Backbone) ->
    class User extends Backbone.Model
        urlRoot: 'api/users'