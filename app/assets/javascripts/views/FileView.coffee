define ['Backbone', 'jQuery', 'Handlebars'], (Backbone, $, Handlebars) ->
        class FileView extends Backbone.View
            tagName: "tr"
            template: Handlebars.compile $('#file-template').html()
            initialize: ->
                @model.bind('change', @render)
                @model.bind('destroy', @render)

            render: =>
                @$el.html @template(@model.toJSON())
                this

            clear: ->
                @model.destroy()