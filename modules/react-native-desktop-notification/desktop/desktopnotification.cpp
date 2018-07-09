/**
 * Copyright (c) 2017-present, Status Research and Development GmbH.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree. An additional grant
 * of patent rights can be found in the PATENTS file in the same directory.
 *
 */

#include "desktopnotification.h"
#include "bridge.h"
#include "eventdispatcher.h"

#include "libsnore/application.h"
#include "libsnore/snore.h"

#include <QCoreApplication>
#include <QDebug>

namespace {
struct RegisterQMLMetaType {
  RegisterQMLMetaType() { qRegisterMetaType<DesktopNotification *>(); }
} registerMetaType;
} // namespace

class DesktopNotificationPrivate {
public:
  Bridge *bridge = nullptr;
  Snore::Application snoreApp;
};

DesktopNotification::DesktopNotification(QObject *parent)
    : QObject(parent), d_ptr(new DesktopNotificationPrivate) {
  d_ptr->snoreApp = Snore::Application(QCoreApplication::applicationName(),
                                       Snore::Icon::defaultIcon());
  d_ptr->snoreApp.addAlert(
      Snore::Alert("NewMessage", Snore::Icon::defaultIcon()));

  if (Snore::SnoreCore::instance().pluginNames().isEmpty()) {
    Snore::SnoreCore::instance().loadPlugins(Snore::SnorePlugin::Backend);
  }

  qDebug() << "List of all loaded Snore plugins: "
           << Snore::SnoreCore::instance().pluginNames();

  Snore::SnoreCore::instance().registerApplication(d_ptr->snoreApp);
  Snore::SnoreCore::instance().setDefaultApplication(d_ptr->snoreApp);

  qDebug() << "Current notification backend: "
           << Snore::SnoreCore::instance().primaryNotificationBackend();

  // Snore::SnoreCore::instance().setPrimaryNotificationBackend(option);
}

DesktopNotification::~DesktopNotification() {
  Snore::SnoreCore::instance().deregisterApplication(d_ptr->snoreApp);
}

void DesktopNotification::setBridge(Bridge *bridge) {
  Q_D(DesktopNotification);
  d->bridge = bridge;
}

QString DesktopNotification::moduleName() { return "DesktopNotification"; }

QList<ModuleMethod *> DesktopNotification::methodsToExport() {
  return QList<ModuleMethod *>{};
}

QVariantMap DesktopNotification::constantsToExport() { return QVariantMap(); }

void DesktopNotification::sendNotification(QString text) {
  Q_D(DesktopNotification);
  qDebug() << "call of DesktopNotification::sendNotification(); !!!";

  Snore::Notification notification(
      d_ptr->snoreApp, d_ptr->snoreApp.alerts()["NewMessage"], "New message",
      text, Snore::Icon::defaultIcon());
  Snore::SnoreCore::instance().broadcastNotification(notification);
}
