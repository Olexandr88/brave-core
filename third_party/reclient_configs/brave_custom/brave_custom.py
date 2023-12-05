# Copyright (c) 2023 The Brave Authors. All rights reserved.
# This Source Code Form is subject to the terms of the Mozilla Public
# License, v. 2.0. If a copy of the MPL was not distributed with this file,
# You can obtain one at https://mozilla.org/MPL/2.0/.

# This file customizes reclient configs generator to support Brave builds.

import os

# Injected by reclient configurator.
Paths: object
ReclientCfg: object
FileUtils: object
ShellTemplate: object


def pre_configure():
    FileUtils.GENERATED_FILE_HEADER = FileUtils.GENERATED_FILE_HEADER.replace(
        "rerun configurator", "run 'npm run sync'")


def merge_reproxy_cfg(reproxy_cfg):
    reproxy_cfg = ReclientCfg.merge_cfg(
        reproxy_cfg,
        {
            # Increase verbosity for rbe debugging.
            'v': 2,
        })

    # Set values from supported RBE_ environment variables.
    SUPPORTED_REPROXY_ENV_VARS = (
        'RBE_service',
        'RBE_service_no_auth',
        'RBE_tls_client_auth_cert',
        'RBE_tls_client_auth_key',
        'RBE_use_application_default_credentials',
    )
    for env_var in SUPPORTED_REPROXY_ENV_VARS:
        value = os.environ.get(env_var)
        if value:
            reproxy_cfg[env_var[4:]] = value

    return reproxy_cfg


def merge_rewrapper_cfg(rewrapper_cfg, tool, _host_os):
    # Enabled canonicalize_working_dir mode replaces directory structure with
    # `set_by_reclient/a/a` instead of `src/out/Default`. Brave builds require
    # `src` dir to be named `src`, otherwise C++ overrides won't work.
    rewrapper_cfg = ReclientCfg.merge_cfg(rewrapper_cfg, {
        'canonicalize_working_dir': 'false',
    })

    if tool == 'python':
        # Python actions require PYTHONPATH to be set during Brave builds. We
        # modify python rewrapper config to add remote wrapper into execution.
        # Remote wrapper will always set PYTHONPATH before running a python
        # executable.
        #
        # The remote wrapper is generated in generate_python_remote_wrapper()
        # function below.
        rewrapper_cfg = ReclientCfg.merge_cfg(
            rewrapper_cfg, {
                'inputs': [
                    ('{src_dir}/buildtools/reclient_cfgs/python/'
                     'python_remote_wrapper'),
                    '{src_dir}/brave/script/import_inline.py',
                    '{src_dir}/brave/script/override_utils.py',
                ],
                'remote_wrapper': ('{src_dir}/buildtools/reclient_cfgs/'
                                   'python/python_remote_wrapper'),
            })

    return rewrapper_cfg


def post_configure():
    generate_python_remote_wrapper()


# Python remote wrapper sets PYTHONPATH during remote execution.
def generate_python_remote_wrapper():
    # Load python remote wrapper template.
    source_file = (f'{Paths.abspath(os.path.dirname(__file__))}/'
                   'python_remote_wrapper.template')
    python_remote_wrapper_template = FileUtils.read_text_file(source_file)

    # Generate PYTHONPATH env variable with paths accessible from exec_root.
    relative_python_paths = []
    for python_path in os.environ.get('PYTHONPATH', '').split(os.pathsep):
        if not python_path:
            continue
        abs_python_path = Paths.abspath(python_path)
        if abs_python_path.startswith(Paths.exec_root):
            relative_python_paths.append(
                Paths.relpath(abs_python_path, Paths.build_dir))

    if not relative_python_paths:
        print('WARNING: PYTHONPATH is empty. Remote python actions will fail.')

    # Variables to replace in the template.
    template_vars = {
        'autogenerated_header': FileUtils.create_generated_header(
            (source_file, Paths.abspath(__file__))),
        'pythonpath': ':'.join(relative_python_paths),
    }

    # Write the python remote wrapper.
    FileUtils.write_text_file(
        (f'{Paths.src_dir}/buildtools/reclient_cfgs/python/'
         'python_remote_wrapper'),
        ShellTemplate(python_remote_wrapper_template).substitute(template_vars))
