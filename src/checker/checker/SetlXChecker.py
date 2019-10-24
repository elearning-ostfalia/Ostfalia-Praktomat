# -*- coding: utf-8 -*-

from pipes import quote
import os
import re
import logging

from django.conf import settings

from django.db import models
from django.utils.translation import ugettext_lazy as _
from django.utils.html import escape
from checker.basemodels import Checker, CheckerResult, CheckerFileField, truncated_log
from utilities.safeexec import execute_arglist
from utilities.file_operations import *

logger = logging.getLogger(__name__)

RXSECURE = re.compile(r"(deleteFile|appendFile|load|readFile|writeFile|stop|assert|trace|ask|\brun\b|eval|execute)",
                      re.MULTILINE)
RXFAIL = re.compile(r"(fail|:\s*false|syntax error|Error|Internal error|Evaluation of iterator)",
                      re.MULTILINE)
class SetlXChecker(Checker):

    name = models.CharField(max_length=100, default="SetlXChecker", help_text=_("Name to be displayed "
                                                                              "on the solution detail page."))
    testFile = CheckerFileField(help_text=_("Test File which is appended to the submission"))

    def title(self):
        """ Returns the title for this checker category. """
        return self.name

    @staticmethod
    def description():
        """ Returns a description for this Checker. """
        return u"Check http://randoom.org/Software/SetlX"

    def secureSubmission(self, submission):
        if RXSECURE.search(submission):
            return False
        else:
            return True

    def conCat(self, testdir, studentSubmission, testFile):
        #if studentSubmission.__class__.__name__ != 'unicode':
        #    raise Exception('unsupported class ' + studentSubmission.__class__.__name__)

        import codecs
        with codecs.open(os.path.join(testdir, "concat.stlx"), encoding='utf-8', mode='w+') as concat:
            logger.debug('studentSubmission class name is ' + studentSubmission.__class__.__name__)
            f = codecs.open(self.testFile.path, encoding='utf-8')
            testfile_content = f.read()
            sequence = [studentSubmission, testfile_content]
            output = ''.join(sequence)
            concat.write(output)
            return concat


    def run(self, env):

        # Setup
        test_dir = env.tmpdir()
        replace = [(u'PROGRAM', env.program())] if env.program() else []
        copy_file(self.testFile.path, os.path.join(test_dir, os.path.basename(self.testFile.path)))
        script_dir = os.path.join(os.path.dirname(os.path.dirname(__file__)), 'scripts')

        # check: only one submission file allowed
        result = self.create_result(env)
        if len(env.sources()) > 1:
            result.set_log("Sie dürfen nur eine Datei angegeben!")
            result.set_passed(False)
            return result

        # check submission
        for (name, content) in env.sources():
            if not(self.secureSubmission(content)):
                result.set_passed(False)
                result.set_log("Bitte keine IO-Befehle verwenden")
                return result
            else:
                #concat test
                #try:
                self.conCat(test_dir, content, self.testFile)
                #except UnicodeEncodeError:
                #    result.set_passed(False)
                #    result.set_log("Special characters can pose a problem. Vermeiden Sie Umlaute im Source Code "
                #                   "und verwenden Sie kein <, > oder & in XML Dokumenten.")
                #    return result


        # complete test
        cmd = [settings.JVM, '-cp', settings.SETLXJAR, "org.randoom.setlx.pc.ui.SetlX", "concat.stlx"]
        # (output, error, exitcode) = execute(args, env.tmpdir())

        environ = {}
        environ['UPLOAD_ROOT'] = settings.UPLOAD_ROOT

        [output, error, exitcode, timed_out, oom_ed] = execute_arglist(cmd, env.tmpdir(),
                                                                           environment_variables=environ,
                                                                           timeout=settings.TEST_TIMEOUT,
                                                                           fileseeklimit=settings.TEST_MAXFILESIZE,
                                                                           extradirs=[script_dir])

        # [output, error, exitcode, timed_out] = execute_arglist(cmd, env.tmpdir(),
                                                           #  use_default_user_configuration=True,
                                                           #  timeout=settings.TEST_TIMEOUT,
                                                           #  fileseeklimit=settings.TEST_MAXFILESIZE,
                                                           #  extradirs=[script_dir])                                                            extradirs=[script_dir])


        (output, truncated) = truncated_log(output)
        # Remove Praktomat-Path-Prefixes from result:
        # output = re.sub(r"^"+re.escape(env.tmpdir())+"/+", "", output, flags=re.MULTILINE)
        output = re.sub(r""+re.escape(env.tmpdir() + "/")+"+", "", output, flags=re.MULTILINE)
        output = output.encode('utf-8').decode('latin-1')

        # TODO use plaintext instead of html
        output = '<pre>' + '\n\n======== Test Results ======\n\n</pre><br/><pre>' + \
                 escape(output) + '</pre>'
        result.set_log(output, timed_out=timed_out, truncated=truncated)
        result.set_passed(not exitcode and not timed_out and (RXFAIL.search(output) is None) and not truncated)

        return result

from checker.admin import CheckerInline


class SetlXCheckerInline(CheckerInline):
    model = SetlXChecker