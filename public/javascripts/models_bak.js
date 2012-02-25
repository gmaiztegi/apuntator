(function(jQuery, window, document, undefined){
    
    var $ = jQuery;
    
    $(function(){
        
        window.File = Backbone.Model.extend({
            defaults: function() {
                return {};
            },
            
            urlRoot: '/api/files'
        });
        
        window.FileList = Backbone.Collection.extend({
            model: File,
            url: '/api/files'
        });
        
        window.Files = new FileList;
        
        window.FileView = Backbone.View.extend({
            tagName: "div",
            template: _.template("<ul><li class=\"name\"></li><li class=\"descr\"></li></ul>"),
            
            initialize: function() {
                this.model.bind('change', this.render, this);
                this.model.bind('destroy', this.remove, this);
            },
            
            render: function() {
                this.$el.html(this.template(this.model.toJSON()));
                this.$('.name').text(this.model.get('name'));
                this.$('.descr').text(this.model.get('description'));
                return this;
            },
            
            clear: function() {
                this.model.destroy();
            }
        });
        
        window.AppView = Backbone.View.extend({
            events: {
            },
            
            initialize: function() {
                this.setElement($("#fileapp"));
                
                Files.bind('add', this.addOne, this);
                Files.bind('reset', this.addAll, this);
                Files.bind('all', this.render, this);
                
                Files.fetch();
            },
            
            render: function() {
            },
            
            addOne: function(file) {
                var view = new FileView({model: file});
                $('#file-list').append(view.render().el);
            },
            
            addAll: function() {
                Files.each(this.addOne);
            }
        });
        
        window.App = new AppView;
    });
    
})(window.jQuery, window, window.document);