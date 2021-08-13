script {
    String TIME = sh(returnStdout: true, script: 'echo $(date +%Y%m%d%H%M%S)').trim();
    // 压缩包名称
    env.TGZ_NAME = "Messaging-${TGZ_VERSION}-${TIME}-${TGZ_REVISION}";
    // 将目录按照分支的名称去创建
    if (env.GERRIT_REFNAME) {
        env.TGZ_VERSION = "maap/${TGZ_VERSION}";
        env.TGZ_NAME = "Messaging-${GERRIT_REFNAME.find(/\d+\.\d+(\.\d+)?/)}-${TIME}-${TGZ_REVISION}";
        env.GERRIT_CHANGE_SUBJECT = "version:【ALL】" // 出正式版本
    } else {
        env.TGZ_NAME = "Messaging-${TGZ_VERSION}-${TIME}-${TGZ_REVISION}";
        env.TGZ_VERSION = env.GERRIT_BRANCH;
    }
}

String TARGET_TGZ = env.TGZ_NAME + ".tgz"

sh """
        mkdir -p shared
        cd ${GERRIT_PROJECT}
        chmod 777 ./build.sh
        ./build.sh
        tar -zcf ${TARGET_TGZ} maap/
        cd -; mv ${GERRIT_PROJECT}/*.tgz shared
    """;
