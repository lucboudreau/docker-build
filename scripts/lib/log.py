import getpass
import logging

def configureLog(level):
  numeric_level = getattr(logging, level.upper(), None)
  if not isinstance(numeric_level, int):
    raise ValueError('Invalid log level: %s' % loglevel)
  logging.basicConfig(format="Wingman - %(levelname)s:%(name)s:%(message)s", level=numeric_level)

def logUser(logger):
  logger.info(' '.join(['Acting as', getpass.getuser()]))
