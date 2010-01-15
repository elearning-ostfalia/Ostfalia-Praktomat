from django.conf.urls.defaults import *
from django.conf import settings
from django.core.urlresolvers import reverse
import sys
import os

from django.contrib import admin
admin.autodiscover()

urlpatterns = patterns('',
	# Index page
	url(r'^$', 'django.views.generic.simple.redirect_to', {'url': 'tasks/'}, name="index"),
	
	# Admin
	(r'^admin/doc/', include('django.contrib.admindocs.urls')),
    (r'^admin/', include(admin.site.urls)),
	url(r'^admin/$', admin.site.root, name="admin"), # allows for url admin
    
	# Login and Registration
	(r'^accounts/', include('praktomat.accounts.urls')),
	
	#Tasks
	url(r'^tasks/$', 'praktomat.tasks.views.taskList', name = 'task_list'),
	url(r'^tasks/(?P<task_id>\d+)/$', 'praktomat.tasks.views.taskDetail', name='taskDetail'),
	url(r'^tasks/(?P<task_id>\d+)/solutionupload/$', 'praktomat.solutions.views.solution_list', name='solution_list'),

	# Solutions
	url(r'^solutions/(?P<solution_id>\d+)/$', 'praktomat.solutions.views.solution_detail', name='solution_detail'),
	url(r'^solutions/(?P<solution_id>\d+)/attestation/$', 'praktomat.attestation.views.new_attestation', name='new_attestation'),

	#Attestation
	url(r'^tasks/(?P<task_id>\d+)/attestation/statistics$', 'praktomat.attestation.views.statistics', name='statistics'),
	url(r'^tasks/(?P<task_id>\d+)/attestation/$', 'praktomat.attestation.views.attestation_list', name='attestation_list'),
	url(r'^attestation/(?P<attestation_id>\d+)/new$', 'praktomat.attestation.views.edit_attestation', name='edit_attestation'),
	url(r'^attestation/(?P<attestation_id>\d+)$', 'praktomat.attestation.views.view_attestation', name='view_attestation'),
)



# only serve static files through django while in development - for safety and speediness
if 'runserver' in sys.argv or 'runserver_plus' in sys.argv: 
    urlpatterns += patterns('',
		(r'^static/(?P<path>.*)$', 'django.views.static.serve', {'document_root': os.path.dirname(__file__) + '/static/'}),
        (r'^(?P<path>.*)$', 'django.views.static.serve', {'document_root': settings.MEDIA_ROOT}),
        (r'^favicon.ico$', 'django.views.static.serve', {'document_root': settings.MEDIA_ROOT, 'path':"favicon.ico"}),
    )
    
